package com.example.chenjianhong.rxjava_introduct.network.api;

import com.example.chenjianhong.rxjava_introduct.model.ZhuangbiImage;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface ZhuangbiApi {
    @GET("search")
    Observable<List<ZhuangbiImage>> search(@Query("q") String query);
}
