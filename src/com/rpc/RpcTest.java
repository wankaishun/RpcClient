package com.rpc;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.rpc.services.HelloService;

public class RpcTest {
	 
    public static void main(String[] args) throws IOException {  
        HelloService service = RPCClient  
                .getRemoteProxyObj(HelloService.class, new InetSocketAddress("localhost", 8088));  
        System.out.println(service.sayHi("test"));  
    }  
}
