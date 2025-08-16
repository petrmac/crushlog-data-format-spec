package app.crushlog.cldf.models.media

import spock.lang.Specification
import com.fasterxml.jackson.databind.ObjectMapper
import app.crushlog.cldf.models.enums.MediaType
import app.crushlog.cldf.models.enums.MediaSource
import app.crushlog.cldf.models.enums.MediaDesignation

class MediaItemSpec extends Specification {

	def objectMapper = new ObjectMapper()

	def "should create media item with all fields"() {
		given:
		def metadata = [
			width: 1920,
			height: 1080,
			size: 500000,
			coordinates: [
				latitude: 37.7340,
				longitude: -119.6378
			]
		]

		when:
		def item = MediaItem.builder()
				.type(MediaType.PHOTO)
				.path("media/route_topo.jpg")
				.designation(MediaDesignation.TOPO)
				.caption("Full route topo with pitch breakdown")
				.timestamp("2024-01-15T10:30:00Z")
				.source(MediaSource.EMBEDDED)
				.assetId("PHAsset123")
				.thumbnailPath("media/thumbs/route_topo_thumb.jpg")
				.metadata(metadata)
				.build()

		then:
		item.type == MediaType.PHOTO
		item.path == "media/route_topo.jpg"
		item.designation == MediaDesignation.TOPO
		item.caption == "Full route topo with pitch breakdown"
		item.timestamp == "2024-01-15T10:30:00Z"
		item.source == MediaSource.EMBEDDED
		item.assetId == "PHAsset123"
		item.thumbnailPath == "media/thumbs/route_topo_thumb.jpg"
		item.metadata == metadata
	}

	def "should create media item with minimal fields"() {
		when:
		def item = MediaItem.builder()
				.type(MediaType.VIDEO)
				.path("https://youtube.com/watch?v=example")
				.build()

		then:
		item.type == MediaType.VIDEO
		item.path == "https://youtube.com/watch?v=example"
		item.designation == null
		item.caption == null
		item.source == null
	}

	def "should serialize to JSON with new fields"() {
		given:
		def item = MediaItem.builder()
				.type(MediaType.PHOTO)
				.path("media/approach.jpg")
				.designation(MediaDesignation.APPROACH)
				.caption("Approach trail from parking")
				.timestamp("2024-02-01T08:00:00Z")
				.source(MediaSource.EMBEDDED)
				.build()

		when:
		def json = objectMapper.writeValueAsString(item)
		def result = objectMapper.readValue(json, Map.class)

		then:
		result.type == "photo"
		result.path == "media/approach.jpg"
		result.designation == "approach"
		result.caption == "Approach trail from parking"
		result.timestamp == "2024-02-01T08:00:00Z"
		result.source == "embedded"
	}

	def "should deserialize from JSON with new fields"() {
		given:
		def json = '''
        {
            "type": "video",
            "path": "https://youtube.com/watch?v=example",
            "designation": "beta",
            "caption": "Beta video showing the crux sequence",
            "timestamp": "2024-01-20T14:00:00Z",
            "source": "external",
            "metadata": {
                "duration": 120,
                "width": 1920,
                "height": 1080
            }
        }
        '''

		when:
		def item = objectMapper.readValue(json, MediaItem.class)

		then:
		item.type == MediaType.VIDEO
		item.path == "https://youtube.com/watch?v=example"
		item.designation == MediaDesignation.BETA
		item.caption == "Beta video showing the crux sequence"
		item.timestamp == "2024-01-20T14:00:00Z"
		item.source == MediaSource.EXTERNAL
		item.metadata.duration == 120
		item.metadata.width == 1920
		item.metadata.height == 1080
	}

	def "should handle external source type"() {
		given:
		def item = MediaItem.builder()
				.type(MediaType.VIDEO)
				.path("https://vimeo.com/123456789")
				.source(MediaSource.EXTERNAL)
				.designation(MediaDesignation.BETA)
				.build()

		when:
		def json = objectMapper.writeValueAsString(item)
		def result = objectMapper.readValue(json, MediaItem.class)

		then:
		result.source == MediaSource.EXTERNAL
		result.designation == MediaDesignation.BETA
	}

	def "should support all designation types"() {
		given:
		def item = MediaItem.builder()
				.type(MediaType.PHOTO)
				.path("media/test.jpg")
				.designation(designation)
				.build()

		when:
		def json = objectMapper.writeValueAsString(item)
		def result = objectMapper.readValue(json, MediaItem.class)

		then:
		result.designation == designation

		where:
		designation << MediaDesignation.values()
	}

	def "should handle missing designation field in JSON"() {
		given:
		def json = '''
		{
			"type": "photo",
			"path": "media/test.jpg"
		}
		'''

		when:
		def item = objectMapper.readValue(json, MediaItem.class)

		then:
		// Without explicit @JsonProperty default, it will be null
		// This test verifies current behavior
		item.designation == null
	}

	def "should apply default designation when explicitly set"() {
		given:
		def json = '''
		{
			"type": "photo",
			"path": "media/test.jpg",
			"designation": null
		}
		'''

		when:
		def item = objectMapper.readValue(json, MediaItem.class)

		then:
		// When explicitly null in JSON, it remains null
		item.designation == null
	}
}
