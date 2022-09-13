package com.bmw.seckill.service;

import com.bmw.seckill.common.base.BaseResponse;
import com.bmw.seckill.model.http.SeckillReq;

public interface SeckillService {

    BaseResponse sOrder(SeckillReq req);

    //悲观锁
    BaseResponse pOrder(SeckillReq req);

    //乐观锁
    BaseResponse oOrder(SeckillReq req) throws Exception;
}
