from django.db import models

class Artefact(models.Model):
	image = models.ImageField(upload_to='images/artefacts/')
	imageFeatures = models.TextField()
	imageDesc = models.TextField()
	lat = models.FloatField('Latitude', blank=True, null=True)
	lon = models.FloatField('Longitude', blank=True, null=True)

	def __unicode__(self):
		return '%s %s' % (self.lat, self.lon)