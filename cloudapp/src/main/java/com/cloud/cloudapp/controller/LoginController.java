package com.cloud.cloudapp.controller;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import com.cloud.cloudapp.entity.Message;
import com.cloud.cloudapp.entity.Users;
import com.cloud.cloudapp.service.UserService;
import com.cloud.cloudapp.service.EmailService;
import java.util.Iterator;

@Controller
public class LoginController 
{
	@Autowired
	private UserService userService;

    @Autowired
    EmailService emailService;
    
	@RequestMapping(value={"/", "/login"}, method = RequestMethod.GET)
	public ModelAndView login(@RequestParam(value = "error", required = false) String error){
		ModelAndView modelAndView = new ModelAndView();
		if (error != null) 
		{
		  modelAndView.setViewName("error page");
		} 
		else
		{
			modelAndView.setViewName("login");
		}
		
		return modelAndView;
	}
	
	@RequestMapping("/login-error.html")
	public String loginError(Model model) {
		model.addAttribute("loginError", true);
		  
		return "login.html";
	}
	
	@GetMapping("/registration")
	public ModelAndView showRegistrationPage() 
	{
		ModelAndView modelAndView = new ModelAndView();
		Users users = new Users();
		
		modelAndView.addObject("user", users);
		modelAndView.setViewName("registration");
		
		return modelAndView;
	}
	
	@PostMapping("/registration")
	public ModelAndView createUser(@Valid Users user, BindingResult bindingResult) 
	{
		ModelAndView modelAndView = new ModelAndView();
		Users userExists = userService.findUserByEmail(user.getEmail());
		
		if(userExists != null) 
		{
			bindingResult
				.rejectValue("email", "error.user", "There is already a user registered with the email provided");
		}
		
		if(bindingResult.hasErrors()) 
		{
			modelAndView.setViewName("registration");
		}
		else 
		{
			userService.saveUser(user);
			modelAndView.addObject("successMessage", "User has been registered succesfully");
			modelAndView.addObject("user", new Users());
			modelAndView.setViewName("registration");
		}
		return modelAndView;
	}
	
	@GetMapping("/home")
	public ModelAndView home() 
	{
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Users user = userService.findUserByEmail(auth.getName());
		ModelAndView modelAndView = new ModelAndView();
		
		if(user.getType().equalsIgnoreCase("ADMIN")) {
			modelAndView.setViewName("adminPageView");
			modelAndView.addObject("message", new Message());
		} else {
			modelAndView.setViewName("userPageView");
		}
		modelAndView.addObject("user", user);

		return modelAndView;
	}
	
	@ResponseBody
	@RequestMapping(value={"/sendAlertToUser"}, method = RequestMethod.POST)
	public void sendAlerToUser(@RequestBody String data, HttpServletRequest request) 
	{
		JSONObject jsonObject = new JSONObject();
		try {
			 jsonObject = new JSONObject(data);
			 String province = (String) jsonObject.get("province");
			 String message = (String) jsonObject.get("messageContent");
			 List<Users> users = userService.findByProvince(province);
			 Iterator iterator = users.iterator();
				
			 while(iterator.hasNext()) {
				 Users tempUser = (Users) iterator.next();
				 Users tempUserForCheck = userService.findUserByEmail(tempUser.getEmail());
				 if(!tempUserForCheck.getLastNotificationForProvince().equalsIgnoreCase(message)) {
				     tempUser.setLastNotificationForProvince(message);
				     userService.saveOnlyUser(tempUser);
				     emailService.sendSimpleMessage(tempUser.getEmail(),"ALERT",message);
				 }	
		     }
		 } catch(Exception e) {
			System.out.println(e);
		 }
	 }
}