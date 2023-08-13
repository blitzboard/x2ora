package x2ora;

//import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.junit.Test;
import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class RetrievalControllerTest {

  private final Context ctx = mock(Context.class);

  @Test
  public void POST_to_list_graphs_gives_201() {
    try {
        RetrievalController.list.handle(ctx);
    } catch (Exception e) {
        e.printStackTrace();
    }
    verify(ctx).status(201);
  }

}
