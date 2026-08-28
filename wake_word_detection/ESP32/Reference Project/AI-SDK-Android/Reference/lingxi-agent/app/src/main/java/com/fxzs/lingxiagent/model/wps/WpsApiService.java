package com.fxzs.lingxiagent.model.wps;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface WpsApiService {

    // --- Document Conversion ---

    @POST("/api/developer/v1/office/convert/to/pdf")
    Observable<WpsTaskResponse> convertToPdf(@Body WpsConvertRequest body);

    @POST("/api/developer/v1/office/convert/to/png")
    Observable<WpsTaskResponse> convertToPng(@Body WpsConvertRequest body);

    @GET("/api/developer/v1/tasks/{task_id}")
    Observable<WpsTaskResponse> getConvertTaskStatus(@Path("task_id") String taskId);


    // --- OCR ---

    @POST("/api/developer/v1/office/pdf/convert/to/{office_type}")
    Observable<WpsTaskResponse> ocrPdfToDocs(@Body WpsConvertRequest bod,@Path("office_type") String officeType);

    @POST("/api/developer/v1/office/img/convert/to/{office_type}")
    Observable<WpsTaskResponse> ocrImgToDocs(@Body WpsConvertRequest body,@Path("office_type") String officeTye);

    @GET("/api/developer/v1/tasks/convert/to/{office_type}/{task_id}")
    Observable<WpsTaskResponse> getOcrTaskStatus(@Path("office_type") String officeTye,@Path("task_id") String taskId);

}
