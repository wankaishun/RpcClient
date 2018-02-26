package com.rpc;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.rpc.handler.ResultHandler;
import com.rpc.pojo.ClassInfo;
import com.rpc.services.HelloService;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class RPCClient<T> {

    @SuppressWarnings("unchecked")  
    public static <T> T getRemoteProxyObj(final Class<?> serviceInterface, final InetSocketAddress addr) {  
        // 1.将本地的接口调用转换成JDK的动态代理，在动态代理中实现接口的远程调用  
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class<?>[] { serviceInterface },  
                new InvocationHandler() {  
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {  
                        ClassInfo classInfo = new ClassInfo();  
                        classInfo.setClassName(serviceInterface.getName());  
                        classInfo.setMethodName(method.getName());  
                        classInfo.setObjects(args);  
                        classInfo.setTypes(method.getParameterTypes());  
                        ResultHandler resultHandler = new ResultHandler();  
                        EventLoopGroup group = new NioEventLoopGroup();    
                        try {    
                            Bootstrap b = new Bootstrap();    
                            b.group(group)    
                             .channel(NioSocketChannel.class)    
                             .option(ChannelOption.TCP_NODELAY, true)    
                             .handler(new ChannelInitializer<SocketChannel>() {    
                                 @Override    
                                 public void initChannel(SocketChannel ch) throws Exception {    
                                    ch.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingDecoder());
     	                            ch.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingEncoder());
     	                            ch.pipeline().addLast(new ReadTimeoutHandler(5));     
     	                            ch.pipeline().addLast("handler",resultHandler);  
                                 }    
                             });    
                            ChannelFuture future = b.connect("localhost", 8088).sync();    
                            future.channel().writeAndFlush(classInfo).sync();  
                            future.channel().closeFuture().sync();    
                        } finally {    
                            group.shutdownGracefully();    
                        }  
                        return resultHandler.getResponse();  
                    }  
                });  
    }  
}
