package com.nowait.applicationadmin.cancelOrder.dto;

import com.nowait.domainadminrdb.cancelOrder.entity.CancelReason;

public record CancelOrderRequest(CancelReason reason) { }
