from django.urls import path, include
from rest_framework import routers
from . import views

router = routers.DefaultRouter()
router.register('Post', views.BlogImages, basename='post')

urlpatterns = [
    path('', views.post_list, name='post_list'),
    path('api_root/', include(router.urls)),
]
