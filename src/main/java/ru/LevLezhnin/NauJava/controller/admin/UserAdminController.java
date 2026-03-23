package ru.LevLezhnin.NauJava.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    private final UserService userService;

    @Autowired
    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> findAllByCriteria(@RequestParam("searchBy") String searchBy,
                                        @RequestParam("search") String search,
                                        @RequestParam("page_size") int pageSize,
                                        @RequestParam("page") int page) {
        return userService.findByCriteria(searchBy, search, page, pageSize);
    }
}
