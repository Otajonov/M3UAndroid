package com.m3u.data.repository.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.m3u.core.architecture.dispatcher.Dispatcher
import com.m3u.core.architecture.dispatcher.M3uDispatchers.IO
import com.m3u.core.architecture.logger.Logger
import com.m3u.core.architecture.logger.execute
import com.m3u.core.wrapper.Resource
import com.m3u.core.wrapper.emitException
import com.m3u.core.wrapper.emitResource
import com.m3u.core.wrapper.resourceFlow
import com.m3u.data.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

private const val BITMAP_QUALITY = 100

class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher
) : MediaRepository {
    private val directory =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "M3U")

    override fun savePicture(url: String): Flow<Resource<File>> = resourceFlow {
        try {
            val drawable = checkNotNull(loadDrawable(url))
            val bitmap = drawable.toBitmap()
            val name = "Picture_${System.currentTimeMillis()}.png"
            directory.mkdirs()
            val file = File(directory, name)
            file.outputStream().buffered().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, BITMAP_QUALITY, it)
                it.flush()
            }
            emitResource(file)
        } catch (e: Exception) {
            logger.log(e)
            emitException(e)
        }
    }
        .flowOn(ioDispatcher)

    override suspend fun loadDrawable(url: String): Drawable? = logger.execute<Drawable> {
        val loader = Coil.imageLoader(context)
        val request: ImageRequest = ImageRequest.Builder(context)
            .data(url)
            .build()
        return when (val result = loader.execute(request)) {
            is SuccessResult -> result.drawable
            is ErrorResult -> throw result.throwable
        }
    }

    override fun openOutputStream(uri: Uri): OutputStream? {
        return context.contentResolver.openOutputStream(uri)
    }

    override fun openInputStream(uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }
}
