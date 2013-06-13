from rest_framework import serializers
from gretel_server.models import Artefact

class ArtefactSerializer(serializers.ModelSerializer):
	class Meta:
		model = Artefact
		fields = ('image', 'lat', 'lon') 