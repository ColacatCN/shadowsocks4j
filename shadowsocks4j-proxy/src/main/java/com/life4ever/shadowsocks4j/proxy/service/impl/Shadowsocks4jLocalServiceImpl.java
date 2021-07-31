package com.life4ever.shadowsocks4j.proxy.service.impl;

import com.life4ever.shadowsocks4j.proxy.handler.local.LocalServerChannelInitializer;
import com.life4ever.shadowsocks4j.proxy.service.AbstractShadowsocks4jService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

import static com.life4ever.shadowsocks4j.proxy.util.ConfigUtil.getLocalServerInetSocketAddress;

public class Shadowsocks4jLocalServiceImpl extends AbstractShadowsocks4jService {

    public Shadowsocks4jLocalServiceImpl() {
        super(getLocalServerInetSocketAddress());
        this.initialize();
    }

    @Override
    protected void initialize() {
        super.initialize();
        serverBootstrap()
                .option(ChannelOption.SO_BACKLOG, 32768)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, false);
    }

    @Override
    protected EventLoopGroup initializeEventLoopGroup(int numOfThreads, ThreadFactory threadFactory) {
        return new NioEventLoopGroup(numOfThreads, threadFactory);
    }

    @Override
    protected ChannelFuture bind(SocketAddress publishSocketAddress) {
        ServerBootstrap serverBootstrap = serverBootstrap();
        serverBootstrap
                .channel(NioServerSocketChannel.class)
                .childHandler(new LocalServerChannelInitializer());
        return serverBootstrap.bind(publishSocketAddress);
    }

}