"""智解星 uniCloud 后端接口冒烟测试（仅使用演示账号，不包含任何真实凭证）。"""
import json
import time
import urllib.error
import urllib.request

BASE = "https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/exam"
SEED = json.load(open("unicloud/exam-seed.json", encoding="utf-8"))
CORRECT = {q["id"]: q["correctAnswer"] for p in SEED["papers"] for q in p["questions"]}


def call(method, path, body=None, token=None):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(BASE + path, data=data, method=method)
    request.add_header("Accept", "application/json")
    if data is not None:
        request.add_header("Content-Type", "application/json; charset=utf-8")
    if token:
        request.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status, json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read().decode("utf-8"))


def check(label, condition, detail=""):
    print(("PASS  " if condition else "FAIL  ") + label + (("  " + detail) if detail else ""))
    if not condition:
        raise SystemExit(1)


status, payload = call("GET", "/api/health")
check("health", status == 200 and payload["code"] == 200, json.dumps(payload.get("data"), ensure_ascii=False))

status, payload = call("POST", "/api/auth/login", {"username": "student", "password": "wrong"})
check("wrong password is not 401", status != 401 and payload["code"] != 200, payload["message"])

status, payload = call("GET", "/api/exam/papers")
check("missing token -> 401", status == 401 and payload["code"] == 401)

status, payload = call("POST", "/api/auth/login", {"username": "student", "password": "123456"})
check("login student", status == 200 and payload["data"]["userInfo"]["role"] == "考生")
token = payload["data"]["token"]

status, payload = call("GET", "/api/exam/papers", token=token)
papers = payload["data"]
check("paper list", status == 200 and len(papers) == 2,
      ", ".join(f"{p['levelName']}:{p['questionCount']}题/及格{p['passScore']}" for p in papers))
basic = papers[0]

status, payload = call("POST", "/api/exam/attempts", {"paperId": basic["id"]}, token)
started = payload["data"]
check("start attempt", status == 200 and len(started["questions"]) == 35,
      f"remaining={started['remainingSeconds']}s")
attempt_id = started["attemptId"]
first = started["questions"][0]

status, payload = call("PUT", f"/api/exam/attempts/{attempt_id}/answer",
                       {"questionId": first["id"], "answer": CORRECT[first["id"]], "isFlagged": True}, token)
check("save answer", status == 200 and payload["code"] == 200)

flags = []
for count in range(1, 4):
    status, payload = call("PUT", f"/api/exam/attempts/{attempt_id}/tab-switch",
                           {"tabSwitchCount": count}, token)
    flags.append((payload["data"]["tabSwitchCount"], payload["data"]["shouldAutoSubmit"]))
check("tab switch adjudication", flags == [(1, False), (2, False), (3, True)], str(flags))

status, payload = call("POST", f"/api/exam/attempts/{attempt_id}/submit",
                       {"submitType": "TAB_SWITCH", "tabSwitchCount": 3}, token)
result = payload["data"]
check("submit", status == 200 and result["submitType"] == "TAB_SWITCH",
      f"score={result['score']} correct={result['correctCount']}/{result['totalQuestions']}")

status, payload = call("GET", f"/api/exam/attempts/{attempt_id}/result", token=token)
check("result", payload["data"]["score"] == result["score"] and payload["data"]["submitType"] == "TAB_SWITCH")

status, payload = call("GET", f"/api/exam/attempts/{attempt_id}/review", token=token)
reviewed = payload["data"]["questions"][0]
check("review", payload["data"]["isPassed"] in (0, 1) and reviewed["isCorrect"] is True,
      f"{reviewed['questionId']} {reviewed['userAnswer']}")

status, payload = call("GET", "/api/exam/attempts?page=1&size=100", token=token)
records = payload["data"]["records"]
check("records", any(r["id"] == attempt_id for r in records), f"total={payload['data']['total']}")

status, payload = call("GET", "/api/exam/certificates", token=token)
check("certificates", payload["code"] == 200, f"count={len(payload['data'])}")

username = "smoke" + str(int(time.time()))[-6:]
status, payload = call("POST", "/api/auth/register",
                       {"username": username, "password": "secret12", "confirmPassword": "secret12"})
check("register", payload["code"] == 200, payload["message"])
status, payload = call("POST", "/api/auth/login", {"username": username, "password": "secret12"})
check("login new account", payload["code"] == 200 and payload["data"]["userInfo"]["username"] == username)

print("ALL PASS")
