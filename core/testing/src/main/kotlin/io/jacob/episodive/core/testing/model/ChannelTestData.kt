package io.jacob.episodive.core.testing.model

import io.jacob.episodive.core.model.Channel

val channelTestData = Channel(
    id = 1,
    title = "CNN Podcasts",
    description = "Exclusive stories and the latest headlines. Exclusive stories and the latest headlines.",
    image = "https://edition.cnn.com/audio/static/images/live/CNN-m.4945681d.png",
    link = "https://edition.cnn.com/audio",
    count = 12,
    podcastGuids = listOf(
        "ed51d808-f6a0-5586-92f1-0cfa4dc6609a",
        "80d15476-f2f6-5e6b-9546-78bb1eb11afd",
        "7a23500f-2196-56a9-b9c1-30654a5ec8aa",
        "fbc06a7f-5215-5c0a-92c9-ebde7baf02b9",
        "2f83e1e7-5838-500f-8508-05c26925fbf0",
        "96b89e36-c55a-5a82-aef0-96ea570e5488",
        "f852a7c0-681d-5a02-bfb4-fd6eb59a6325",
        "3267b289-e802-5fe0-b502-7ade15e5b024",
        "6f045740-91cb-50dc-85e9-0337b49903e5",
        "122e9749-dabc-5fd7-9298-ad7517df30e0",
        "20d5cc86-6d40-5149-a23d-db89c25cc9e4",
        "2e2db455-7d3f-5f94-958b-6bdaf5b5d812"
    )
)

val channelTestDataList = List(10) { channelTestData }