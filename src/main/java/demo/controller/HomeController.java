package demo.controller;


import demo.service.MapService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by sfrensley on 4/16/14.
 */
@Controller
public class HomeController {

    @RequestMapping(value = {"/","/home"})
    public String home(Model model) {

        //Inform view of correct paths
        model.addAttribute("MAP_COMMAND_PATH", "/app" + MapService.MAP_COMMAND_PATH);
        model.addAttribute("MAP_ITEM_PATH", "/app" + MapService.MAP_ITEM_PATH);
        model.addAttribute("MAP_UPDATE_PATH", MapService.MAP_UPDATE_PATH);
        model.addAttribute("MAP_ERROR_PATH", "/user" + MapService.MAP_ERROR_PATH);

        return "home";
    }

}
