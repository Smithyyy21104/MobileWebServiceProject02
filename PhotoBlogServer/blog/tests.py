from io import BytesIO
from tempfile import TemporaryDirectory

from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.utils import timezone
from PIL import Image
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Post


class BlogAPITests(APITestCase):

    def setUp(self):
        self.user = get_user_model().objects.create_user(
            username="testuser",
            password="TestPass123!"
        )

        self.post = Post.objects.create(
            author=self.user,
            title="Sample Photo",
            text="This is a test photograph.",
            published_date=timezone.now(),
            image="blog_image/sample.jpg"
        )

        self.api_url = "/api_root/Post/"

        self.media_dir = TemporaryDirectory()
        self.addCleanup(self.media_dir.cleanup)

        self.media_settings = override_settings(
            MEDIA_ROOT=self.media_dir.name
        )
        self.media_settings.enable()
        self.addCleanup(self.media_settings.disable)

    def create_image(self):
        image = Image.new("RGB", (20, 20), "blue")
        buffer = BytesIO()
        image.save(buffer, format="PNG")

        return SimpleUploadedFile(
            "test_photo.png",
            buffer.getvalue(),
            content_type="image/png"
        )

    def test_get_photo_list(self):
        response = self.client.get(self.api_url)

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )
        self.assertEqual(len(response.data), 1)
        self.assertEqual(
            response.data[0]["title"],
            "Sample Photo"
        )

    def test_get_photo_detail(self):
        url = f"{self.api_url}{self.post.id}/"
        response = self.client.get(url)

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )
        self.assertEqual(
            response.data["title"],
            "Sample Photo"
        )

    def test_get_api_root(self):
        response = self.client.get("/api_root/")

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )

    def test_unauthorized_upload(self):
        response = self.client.post(
            self.api_url,
            {
                "author": self.user.id,
                "title": "Unauthorized Photo",
                "text": "Upload without login",
                "image": self.create_image()
            },
            format="multipart"
        )

        self.assertIn(response.status_code, [401, 403])
        self.assertEqual(Post.objects.count(), 1)

    def test_authorized_upload(self):
        self.client.force_authenticate(user=self.user)

        response = self.client.post(
            self.api_url,
            {
                "author": self.user.id,
                "title": "Uploaded Photo",
                "text": "Automatic upload test",
                "published_date": timezone.now().isoformat(),
                "image": self.create_image()
            },
            format="multipart"
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_201_CREATED
        )
        self.assertEqual(Post.objects.count(), 2)
        self.assertEqual(
            response.data["title"],
            "Uploaded Photo"
        )
        self.assertIn("test_photo", response.data["image"])

    def test_invalid_post_data(self):
        self.client.force_authenticate(user=self.user)

        response = self.client.post(
            self.api_url,
            {
                "author": self.user.id,
                "title": "",
                "text": "Invalid photo"
            },
            format="multipart"
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_400_BAD_REQUEST
        )
        self.assertEqual(Post.objects.count(), 1)

    def test_homepage(self):
        response = self.client.get("/")

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )
        self.assertContains(response, "Sample Photo")
