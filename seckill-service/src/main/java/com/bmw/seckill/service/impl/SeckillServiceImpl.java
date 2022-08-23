package com.bmw.seckill.service.impl;

import com.bmw.seckill.common.base.BaseResponse;
import com.bmw.seckill.common.exception.ErrorMessage;
import com.bmw.seckill.dao.SeckillOrderDao;
import com.bmw.seckill.dao.SeckillProductsDao;
import com.bmw.seckill.dao.SeckillUserDao;
import com.bmw.seckill.model.SeckillOrder;
import com.bmw.seckill.model.SeckillProducts;
import com.bmw.seckill.model.http.SeckillReq;
import com.bmw.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillOrderDao seckillOrderDao;

    @Autowired
    private SeckillProductsDao seckillProductsDao;

    @Autowired
    private SeckillUserDao seckillUserDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse sOrder(SeckillReq req) {
        log.info("===[开始调用原始下单接口~]===");
        //参数校验
        log.info("===[校验用户信息及商品信息]===");
        BaseResponse paramValidRes = validateParam(req.getProductId(), req.getUserId());
        if (paramValidRes.getCode() != 0) {
            return paramValidRes;
        }
        log.info("===[校验参数是否合法][通过]===");

        Long productId = req.getProductId();
        Long userId = req.getUserId();
        SeckillProducts product = seckillProductsDao.selectByPrimaryKey(productId);
        Date date = new Date();
        // 扣减库存
        log.info("===[开始扣减库存]===");
        product.setSaled(product.getSaled() + 1);
        seckillProductsDao.updateByPrimaryKeySelective(product);
        log.info("===[扣减库存][成功]===");
        // 创建订单
        log.info("===[开始创建订单]===");
        SeckillOrder order = new SeckillOrder();
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setUserId(userId);
        order.setCreateTime(date);
        seckillOrderDao.insert(order);
        log.info("===[创建订单][成功]===");
        return BaseResponse.OK;
    }


    private BaseResponse validateParam(Long productId, Long userId) {
        SeckillProducts product = seckillProductsDao.selectByPrimaryKey(productId);
        if (product == null) {
            log.error("===[产品不存在！]===");
            return BaseResponse.error(ErrorMessage.SYS_ERROR);
        }

        if (product.getStartBuyTime().getTime() > System.currentTimeMillis()) {
            log.error("===[秒杀还未开始！]===");
            return BaseResponse.error(ErrorMessage.SECKILL_NOT_START);
        }

        if (product.getSaled() >= product.getCount()) {
            log.error("===[库存不足！]===");
            return BaseResponse.error(ErrorMessage.STOCK_NOT_ENOUGH);
        }

        return BaseResponse.OK;
    }
}
