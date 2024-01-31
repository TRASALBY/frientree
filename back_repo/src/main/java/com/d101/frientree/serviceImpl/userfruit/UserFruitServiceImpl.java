package com.d101.frientree.serviceImpl.userfruit;

import com.d101.frientree.dto.userfruit.request.UserFruitTextRequest;
import com.d101.frientree.dto.userfruit.response.UserFruitSaveResponse;
import com.d101.frientree.exception.userfruit.NaverClovaAPIException;
import com.d101.frientree.exception.userfruit.PythonAPIException;
import com.d101.frientree.service.UserFruitService;
import com.d101.frientree.serviceImpl.userfruit.clova.ClovaSpeechClient;
import com.d101.frientree.serviceImpl.userfruit.clova.ClovaSpeechResponse;
import com.d101.frientree.serviceImpl.userfruit.fastapi.HttpPostAIRequest;
import com.d101.frientree.serviceImpl.userfruit.objectstorage.AwsS3ObjectStorage;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Log4j2
public class UserFruitServiceImpl implements UserFruitService {
    //naver clova speech to text class
    private final ClovaSpeechClient clovaSpeechClient;
    private final HttpPostAIRequest httpPostAIRequest;
    private final AwsS3ObjectStorage awsS3ObjectStorage;
    // 생성자를 통한 의존성 주입
    public UserFruitServiceImpl(ClovaSpeechClient clovaSpeechClient, HttpPostAIRequest httpPostAIRequest, AwsS3ObjectStorage awsS3ObjectStorage) {
        this.clovaSpeechClient = clovaSpeechClient;
        this.httpPostAIRequest = httpPostAIRequest;
        this.awsS3ObjectStorage = awsS3ObjectStorage;
    }

    @Override
    public ResponseEntity<UserFruitSaveResponse> speechToTextAudio(MultipartFile file) throws Exception {

       log.info("filename : {}", file.getOriginalFilename());

        // 1. 음성 파일 Aws S3 저장
        String awsS3Path = awsS3ObjectStorage.uploadFile(file);

        // NestRequestEntity 설정 (옵션)
        ClovaSpeechClient.NestRequestEntity requestEntity = new ClovaSpeechClient.NestRequestEntity();
        // 필요한 경우 추가 설정을 여기에 입력
        // 필요한 설정들을 설정
        requestEntity.setLanguage("ko-KR"); // 언어 설정
        requestEntity.setCompletion("sync"); // 완료 유형 설정 (동기/비동기)
        //requestEntity.setCallback("your_callback_url"); // 콜백 URL 설정 (선택 사항)
        // requestEntity.setUserdata(userdataMap); // 사용자 데이터 설정 (선택 사항)
        requestEntity.setWordAlignment(true); // 단어 정렬 설정
        requestEntity.setFullText(true); // 전체 텍스트 인식 결과 출력 설정

        // 발화자 구분(Diarization) 설정 (선택 사항)
        ClovaSpeechClient.Diarization diarization = new ClovaSpeechClient.Diarization();
        diarization.setEnable(false); // 화자 인식 활성화
        //diarization.setSpeakerCountMin(2); // 최소 발화자 수 설정
        //diarization.setSpeakerCountMax(5); // 최대 발화자 수 설정
        requestEntity.setDiarization(diarization);

        // 이벤트 탐지(Sed) 설정 (선택 사항)
        ClovaSpeechClient.Sed sed = new ClovaSpeechClient.Sed();
        sed.setEnable(false); // 이벤트 탐지 활성화
        requestEntity.setSed(sed);

        String result = "";
        try{
            // 2. 파일 업로드 및 인식 요청
            result = clovaSpeechClient.url(awsS3Path, requestEntity);
        }catch (NaverClovaAPIException e){
            throw new NaverClovaAPIException("Naver API Error");
        }
        // 3. 파일 삭제
        awsS3ObjectStorage.deleteImage(awsS3Path);

        // Gson을 사용하여 응답을 ClovaSpeechResponse 객체로 파싱
        Gson gson = new Gson();
        ClovaSpeechResponse response = gson.fromJson(result, ClovaSpeechResponse.class);

        // 전체 텍스트 추출
        String fullText = response.getFullText();

        log.info("음성 Text : {}", response.getFullText());
        try {
            //Python 감정 분석 API 호출
            return ResponseEntity.ok(httpPostAIRequest.sendPostRequest(fullText));
        }catch (PythonAPIException e){
            throw new PythonAPIException("Python AI API Error");
        }
    }
    @Override
    public ResponseEntity<UserFruitSaveResponse> speechToTextText(UserFruitTextRequest textFile) throws Exception {
        try {
            //Python 감정 분석 API 호출
            return ResponseEntity.ok(httpPostAIRequest.sendPostRequest(textFile.getContent()));
        }catch (PythonAPIException e){
            throw new PythonAPIException("Python AI API Error");
        }
    }
}
