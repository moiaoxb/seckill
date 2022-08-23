package com.bmw.seckill.service;

import com.bmw.seckill.common.base.BaseResponse;
import com.bmw.seckill.model.http.SeckillReq;

public interface SeckillService {

    BaseResponse sOrder(SeckillReq req);
}
