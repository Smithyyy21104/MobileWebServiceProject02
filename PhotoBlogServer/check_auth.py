
import json
import getpass
import urllib.request
import urllib.error

username = input("Username: ")
password = getpass.getpass("Password: ")

data = json.dumps({
    "username": username,
    "password": password
}).encode("utf-8")

request = urllib.request.Request(
    "http://127.0.0.1:8000/api-token-auth/",
    data=data,
    headers={"Content-Type": "application/json"},
    method="POST"
)

try:
    with urllib.request.urlopen(request, timeout=10) as response:
        result = json.load(response)
        print("HTTP Status:", response.status)
        if result.get("token"):
            print("Token authentication: SUCCESS")
        else:
            print("Token authentication: FAILED")

except urllib.error.HTTPError as error:
    print("HTTP Status:", error.code)
    print("Authentication failed.")

except OSError as error:
    print("Connection error:", error)
