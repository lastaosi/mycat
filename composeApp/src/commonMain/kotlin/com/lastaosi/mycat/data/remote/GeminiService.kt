package com.lastaosi.mycat.data.remote

import com.lastaosi.mycat.util.L
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Request 모델
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data")
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    @SerialName("mime_type")
    val mimeType: String,
    val data: String  // base64 인코딩된 이미지
)

// Response 모델
@Serializable
// Response 모델에 error 추가
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)
@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null
)

// 결과 모델
data class BreedRecognitionResult(
    val breedName: String,
    val confidence: Double,
    val description: String
)

/**
 * Gemini API를 통해 고양이 사진에서 품종을 인식하는 서비스.
 *
 * - 모델: gemini-2.5-flash-lite
 * - 입력: JPEG ByteArray (Base64 인코딩 후 inline_data로 전송)
 * - 출력: breedName(한국어), confidence(0~1), description
 * - JSON 파싱은 Regex 방식 사용 (kotlinx.serialization 미적용)
 */
class GeminiService(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"

    suspend fun recognizeBreed(imageBytes: ByteArray): Result<BreedRecognitionResult> {
        return try {
            val base64Image = encodeBase64(imageBytes)
            L.d("Gemini 요청 시작 - 이미지 크기: ${imageBytes.size} bytes")
            L.d("API Key 앞 10자리: ${apiKey.take(10)}...")
            val prompt = """
                이 고양이 사진을 보고 품종을 분석해줘.
                반드시 아래 JSON 형식으로만 답해줘. 다른 설명은 하지 마.
                
                {
                  "breedName": "한국어 품종명",
                  "confidence": 0.85,
                  "description": "품종 특징 한 줄 설명"
                }
                
                - breedName: 한국어 품종명 (예: 코리안 숏헤어, 페르시안, 모름)
                - confidence: 0.0~1.0 사이 신뢰도
                - description: 품종 특징 간단 설명
                
                품종을 알 수 없으면 breedName을 "모름"으로 해줘.
            """.trimIndent()

            val response = httpClient.post("$baseUrl?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(
                    GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                parts = listOf(
                                    GeminiPart(
                                        inlineData = GeminiInlineData(
                                            mimeType = "image/jpeg",
                                            data = base64Image
                                        )
                                    ),
                                    GeminiPart(text = prompt)
                                )
                            )
                        )
                    )
                )
            }.body<GeminiResponse>()
            L.d("Gemini 응답 전체: $response")
            if (response.error != null) {
                L.e("Gemini API 에러: code=${response.error.code}, message=${response.error.message}, status=${response.error.status}")
                return Result.failure(Exception("API 에러: ${response.error.message}"))
            }

            L.d("Gemini 응답: candidates 수 = ${response.candidates?.size}")
            L.d("Gemini 응답 전체: $response")
            val responseText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?.removePrefix("```json")
                ?.removeSuffix("```")
                ?.trim()
                ?: return Result.failure(Exception("응답이 없습니다."))
            L.d("파싱된 텍스트: $responseText")
            Result.success(parseResponse(responseText))

        } catch (e: Exception) {
            L.e("Gemini 호출 예외: ${e::class.simpleName} - ${e.message}")
            L.e(e)
            Result.failure(e)
        }
    }

    private fun parseResponse(json: String): BreedRecognitionResult {
        val breedName = Regex("\"breedName\"\\s*:\\s*\"([^\"]+)\"")
            .find(json)?.groupValues?.get(1) ?: "모름"
        val confidence = Regex("\"confidence\"\\s*:\\s*([0-9.]+)")
            .find(json)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val description = Regex("\"description\"\\s*:\\s*\"([^\"]+)\"")
            .find(json)?.groupValues?.get(1) ?: ""
        return BreedRecognitionResult(breedName, confidence, description)
    }
}

// base64 인코딩 — commonMain에서 플랫폼 무관하게 동작
expect fun encodeBase64(bytes: ByteArray): String