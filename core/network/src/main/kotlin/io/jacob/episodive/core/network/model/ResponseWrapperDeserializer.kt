package io.jacob.episodive.core.network.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Podcast Index API는 단일 조회 엔드포인트(예: `podcasts/byfeedid`, `episodes/byid`)에서
 * 일치하는 결과가 없을 때 `"feed"`(또는 `"item"`, `"episode"`) 필드를 단일 객체 대신
 * 빈 배열 `[]`로 반환한다.
 *
 * 예:
 *   {"status":"true","query":{"id":"7824745"},"feed":[],"description":"No feeds match this id."}
 *
 * 기본 Gson 역직렬화기는 이 경우 `BEGIN_OBJECT`를 기대하다가 `BEGIN_ARRAY`를 만나
 * `JsonSyntaxException: Expected BEGIN_OBJECT but was BEGIN_ARRAY at ... path $.feed` 예외를 던진다.
 *
 * 이 디시리얼라이저는 해당 필드가 배열이면 `data = null` 로 매핑하여
 * 호출 측이 `ResponseWrapper.data: T?` 의 null 계약으로 "no match" 응답을 안전하게 처리하도록 한다.
 */
class ResponseWrapperDeserializer : JsonDeserializer<ResponseWrapper<Any>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): ResponseWrapper<Any> {
        val jsonObject = json.asJsonObject
        val status = jsonObject.get("status")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
        val description = jsonObject.get("description")?.takeUnless { it.isJsonNull }?.asString.orEmpty()

        val dataElement = FIELD_NAMES
            .asSequence()
            .mapNotNull { jsonObject.get(it) }
            .firstOrNull()

        val data: Any? = when {
            dataElement == null || dataElement.isJsonNull -> null
            // Podcast Index API는 단일 조회에 매칭이 없을 때 `[]` 를 반환한다 → null 로 취급
            dataElement.isJsonArray -> null
            else -> {
                val parameterized = typeOfT as? ParameterizedType
                    ?: throw JsonParseException("ResponseWrapper must be used with a parameterized type")
                val actualType = parameterized.actualTypeArguments[0]
                context.deserialize<Any>(dataElement, actualType)
            }
        }

        return ResponseWrapper(
            status = status,
            data = data,
            description = description,
        )
    }

    companion object {
        private val FIELD_NAMES = listOf("feed", "item", "episode")
    }
}
