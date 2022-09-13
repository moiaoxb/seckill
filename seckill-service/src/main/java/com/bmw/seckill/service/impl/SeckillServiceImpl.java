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

//        log.info("===[校验 用户是否重复下单]===");
//        SeckillOrder param = new SeckillOrder();
//        param.setProductId(req.getProductId());
//        param.setUserId(req.getUserId());
//        int repeatCount = seckillOrderDao.count(param);
//        if (repeatCount > 0) {
//            log.error("===[该用户重复下单！]===");
//            return BaseResponse.error(ErrorMessage.REPEAT_ORDER_ERROR);
//        }
//        log.info("===[校验 用户是否重复下单][通过校验]===");

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

    /**
     * 悲观锁实现方式
     *
     * @param req
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse pOrder(SeckillReq req) {
        log.info("===[开始调用秒杀接口(悲观锁)]===");
        //校验用户信息、商品信息、库存信息
        log.info("===[校验用户信息、商品信息、库存信息]===");
        BaseResponse paramValidRes = validateParamPessimistic(req.getProductId(), req.getUserId());
        if (paramValidRes.getCode() != 0) {
            return paramValidRes;
        }
        log.info("===[校验][通过]===");

        log.info("===[校验 用户是否重复下单]===");
        SeckillOrder param = new SeckillOrder();
        param.setProductId(req.getProductId());
        param.setUserId(req.getUserId());
        int repeatCount = seckillOrderDao.count(param);
        if (repeatCount > 0) {
            log.error("===[该用户重复下单！]===");
            return BaseResponse.error(ErrorMessage.REPEAT_ORDER_ERROR);
        }
        log.info("===[校验 用户是否重复下单][通过校验]===");

        Long userId = req.getUserId();
        Long productId = req.getProductId();
        SeckillProducts product = seckillProductsDao.selectByPrimaryKey(productId);
        // 下单逻辑
        log.info("===[开始下单逻辑]===");
        Date date = new Date();
        // 扣减库存
        product.setSaled(product.getSaled() + 1);
        seckillProductsDao.updateByPrimaryKeySelective(product);
        // 创建订单
        SeckillOrder order = new SeckillOrder();
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setUserId(userId);
        order.setCreateTime(date);
        seckillOrderDao.insert(order);
        return BaseResponse.OK;
    }
    // 悲观锁实现的校验逻辑
    private BaseResponse validateParamPessimistic(Long productId, Long userId) {
        //悲观锁，利用selectForUpdate方法锁定记录，并获得最新的SeckillProducts记录
        SeckillProducts product = seckillProductsDao.selectForUpdate(productId);
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

    //乐观锁
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse oOrder(SeckillReq req) throws Exception {
        log.info("===[开始调用下单接口~（乐观锁）]===");
        //参数校验
        log.info("===[校验参数是否合法]===");
        BaseResponse paramValidRes = validateParam(req.getProductId(), req.getUserId());
        if (paramValidRes.getCode() != 0) {
            return paramValidRes;
        }
        log.info("===[校验参数是否合法][通过]===");
        //下单（乐观锁）
        return createOptimisticOrder(req.getProductId(), req.getUserId());
    }
    private BaseResponse createOptimisticOrder(Long productId, Long userId) throws Exception {
        log.info("===[下单逻辑Starting]===");
        // 创建订单
        SeckillProducts products = seckillProductsDao.selectByPrimaryKey(productId);
        Date date = new Date();
        SeckillOrder order = new SeckillOrder();
        order.setProductId(productId);
        order.setProductName(products.getName());
        order.setUserId(userId);
        order.setCreateTime(date);
        seckillOrderDao.insert(order);
        log.info("===[创建订单成功]===");
        //扣减库存
        int res = seckillProductsDao.updateStockByOptimistic(productId);
        if (res == 0) {
            log.error("===[秒杀失败，抛出异常，执行回滚逻辑！]===");
            throw new Exception("库存不足");
//          return BaseResponse.error(ErrorMessage.SECKILL_FAILED);
        }
        log.info("===[扣减库存成功!]===");
        return BaseResponse.OK;
    }
}
