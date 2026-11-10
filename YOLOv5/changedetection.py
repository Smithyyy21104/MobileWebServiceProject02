
import os
import cv2
import time
import getpass
import requests

from pathlib import Path
from datetime import datetime


class ChangeDetection:
    def __init__(self, names):
        self.result_prev = [0] * len(names)
        self.last_uploaded = {}
        self.cooldown_seconds = 20

        self.host = os.getenv(
            "DJANGO_HOST",
            "http://127.0.0.1:8000"
        ).rstrip("/")

        username = os.getenv("DJANGO_USERNAME")
        if not username:
            username = input("Django username: ").strip()

        password = os.getenv("DJANGO_PASSWORD")
        if not password:
            password = getpass.getpass("Django password: ")

        response = requests.post(
            self.host + "/api-token-auth/",
            data={
                "username": username,
                "password": password
            },
            timeout=15
        )

        response.raise_for_status()
        self.token = response.json()["token"]

        print("Django authentication successful.")

    def add(self, names, detected_current, save_dir, image):
        if len(detected_current) != len(self.result_prev):
            raise ValueError("Detection class count mismatch.")

        appeared = []
        now = time.monotonic()

        for i in range(len(detected_current)):
            name = str(names[i])

            if self.result_prev[i] == 0 and detected_current[i] == 1:
                last_time = self.last_uploaded.get(
                    name,
                    -float("inf")
                )

                if now - last_time >= self.cooldown_seconds:
                    appeared.append(name)

        self.result_prev = list(detected_current)

        if appeared and image is not None:
            self.send(save_dir, image, appeared)

            uploaded_at = time.monotonic()

            for name in appeared:
                self.last_uploaded[name] = uploaded_at

    def send(self, save_dir, image, appeared):
        now = datetime.now()

        save_path = (
            Path(save_dir)
            / "detected"
            / now.strftime("%Y/%m/%d")
        )

        save_path.mkdir(parents=True, exist_ok=True)

        filename = now.strftime("%H%M%S_%f") + ".jpg"
        full_path = save_path / filename

        resized = cv2.resize(
            image,
            (320, 240),
            interpolation=cv2.INTER_AREA
        )

        if not cv2.imwrite(str(full_path), resized):
            raise RuntimeError("Could not save detection image.")

        title = ", ".join(appeared)

        data = {
            "title": title,
            "text": "YOLOv5 detected: " + title
        }

        headers = {
            "Authorization": "Token " + self.token
        }

        with open(full_path, "rb") as image_file:
            response = requests.post(
                self.host + "/api_root/Post/",
                data=data,
                files={
                    "image": (
                        filename,
                        image_file,
                        "image/jpeg"
                    )
                },
                headers=headers,
                timeout=30
            )

        response.raise_for_status()

        print(
            "Detection uploaded:",
            title,
            "| HTTP",
            response.status_code
        )
