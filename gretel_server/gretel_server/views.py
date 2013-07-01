from rest_framework import viewsets, generics
from gretel_server.serializers import ArtefactSerializer
from gretel_server.models import Artefact


class ArtefactViewSet(viewsets.ModelViewSet):
	queryset = Artefact.objects.all()
	serializer_class = ArtefactSerializer