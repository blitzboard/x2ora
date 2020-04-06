package x2oracle;

import io.javalin.Javalin;

public class HelloWorld {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        app.get("/1/", ctx -> ctx.result(ctx.queryParam("name")));
        app.get("/2/", ctx -> ctx.result("Hello World"));
    }
}