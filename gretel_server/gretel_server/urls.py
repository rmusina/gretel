from django.conf.urls import patterns, include, url
from rest_framework import routers
from gretel_server import views
from gretel_server import settings

# from django.contrib import admin
# admin.autodiscover()
router = routers.DefaultRouter()
router.register(r'artefacts', views.ArtefactViewSet)

urlpatterns = patterns('',
    # url(r'^admin/', include(admin.site.urls)),
    url(r'^', include(router.urls)),
    url(r'^api-auth/', include('rest_framework.urls', namespace='rest_framework')),
)

if settings.DEBUG:
    urlpatterns += patterns('',
        (r'^media/(?P<path>.*)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT}))
