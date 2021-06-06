

public class FunctionalTest {

  private JavalinApp app = new JavalinApp();
  private String usersJson = JavalinJson.toJson(UserController.users);

  @Test
  public void GET_to_fetch_users_returns_list_of_users() {
      app.start(1234);
      HttpResponse response = Unirest.get("http://localhost:7000/node_match/?node_ids[]=Taro&limit=100").asString();
      assertThat(response.getStatus()).isEqualTo(200);
      //assertThat(response.getBody()).isEqualTo(usersJson);
      app.stop();
  }

}