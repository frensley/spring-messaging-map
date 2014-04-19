package demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by sfrensley on 4/19/14.
 */
@Controller
public class AuthenticationController {

    @RequestMapping(value = {"/login"})
    public String login() {
        return "login";
    }
}
