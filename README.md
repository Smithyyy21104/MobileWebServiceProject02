# Mobile/Web Service Project

## NOIR FRAME - Photography Journal

A mobile and web service project using Django REST Framework and Android Studio.

## Project Structure

MobileWebServiceProject/
- PhotoBlogServer/ - Django server and REST API
- PhotoViewer/ - Android client application

## Technologies

### Backend
- Python 3.12
- Django 5.2.18
- Django REST Framework 3.18.3
- SQLite
- Pillow
- Coverage.py

### Android
- Java
- Android Studio
- XML layouts
- HTTP API integration

## Main Features

1. Create and manage photography posts.
2. Upload images to the Django server.
3. Retrieve photographs using REST API.
4. Display photographs in the Android application.
5. Authenticate users using Django tokens.
6. Run automated Django API tests.

## Django Server Setup

Open a terminal:

    cd PhotoBlogServer
    python3 -m venv .venv
    source .venv/bin/activate
    python -m pip install -r requirements.txt
    python manage.py migrate
    python manage.py runserver

Local server:

http://127.0.0.1:8000/

## API Endpoints

Photograph list and upload:

http://127.0.0.1:8000/api_root/Post/

Token authentication:

http://127.0.0.1:8000/api-token-auth/

Django administration:

http://127.0.0.1:8000/admin/

## Android Application

Open the PhotoViewer folder in Android Studio.

1. Wait for Gradle synchronization.
2. Start an Android emulator.
3. Run the application.
4. Tap Sync Photographs to retrieve posts.
5. Use Upload Photograph to create a new post.

Current Android cloud server:

https://samz21.pythonanywhere.com/

Cloud REST API:
https://samz21.pythonanywhere.com/api_root/Post/

Cloud Django Admin:
https://samz21.pythonanywhere.com/admin/

The Android application connects to PythonAnywhere using HTTPS.

## Automated Testing

Run:

    cd PhotoBlogServer
    python manage.py test blog

Coverage report:

    python -m coverage run --source=blog manage.py test blog
    python -m coverage report -m
    python -m coverage html

## Development Notes

- The SQLite database and uploaded media are excluded from Git.
- Django generates a local development secret key if needed.
- The Android application uses HTTPS for cloud synchronization and uploads.
- Production deployment requires HTTPS and secure Django settings.

## Course Submission

Mobile/Web Service Project - Common Evaluation 01

Source code includes the Django server and Android client.
