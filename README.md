Kangaroo Project
Netty is an asynchronous event-driven network application framework for rapid development of maintainable high performance protocol servers & clients.

Links
Web Site
Downloads
Documentation
@netty_project
How to build
For the detailed information about building and developing Netty, please visit the developer guide. This page only gives very basic information.

You require the following to build Netty:

Latest stable Oracle JDK 8
Latest stable Apache Maven
If you are on Linux, you need additional development packages installed on your system, because you'll build the native transport.
Note that this is build-time requirement. JDK 5 (for 3.x) or 6 (for 4.0+) is enough to run your Netty-based application.

Branches to look
Development of all versions takes place in each branch whose name is identical to <majorVersion>.<minorVersion>. For example, the development of 3.9 and 4.0 resides in the branch '3.9' and the branch '4.0' respectively.

Usage with JDK 9
Netty can be used in modular JDK9 applications as a collection of automatic modules. The module names follow the reverse-DNS style, and are derived from subproject names rather than root packages due to historical reasons. They are listed below:




public class Main {

    public static void main(String...args){

            Handler.Context context = new DefaultHandler.Builder()
                .build();


            RunProc proc = NettyHttpProducer.base()
                .keepalived()
                .run(context.handler().toObserver)



    }
}
