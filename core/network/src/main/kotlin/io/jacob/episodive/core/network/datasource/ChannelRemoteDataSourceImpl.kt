package io.jacob.episodive.core.network.datasource

import android.content.Context
import io.jacob.episodive.core.common.Dispatcher
import io.jacob.episodive.core.common.EpisodiveDispatchers
import io.jacob.episodive.core.network.model.ChannelResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class ChannelRemoteDataSourceImpl @Inject constructor(
    private val context: Context,
    @param:Dispatcher(EpisodiveDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ChannelRemoteDataSource {
    override suspend fun getChannelById(id: Long): ChannelResponse? {
        Timber.i("getChannelById id: $id")
        return getChannels().find { it.id == id }
    }

    override suspend fun getChannels(): List<ChannelResponse> {
        Timber.i("getChannels")
        return loadJsonAsset<List<ChannelResponse>>(CHANNELS_ASSET)
    }

    private suspend inline fun <reified T> loadJsonAsset(jsonFileName: String): T =
        withContext(ioDispatcher) {
            context.assets.open(jsonFileName)
                .bufferedReader()
                .use { Json.decodeFromString<T>(it.readText()) }
        }

    companion object {
        private const val CHANNELS_ASSET = "channels.json"
    }
}