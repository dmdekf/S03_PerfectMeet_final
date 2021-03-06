package com.POM.MatNam.user.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.POM.MatNam.response.BasicResponse;
import com.POM.MatNam.response.ErrorResponse;
import com.POM.MatNam.review.DAO.ReviewDao;
import com.POM.MatNam.review.DTO.Review;
import com.POM.MatNam.storeres.DAO.StoreResDao;
import com.POM.MatNam.storeres.DTO.StoreRes;
import com.POM.MatNam.user.dao.UserDao;
import com.POM.MatNam.user.dto.FindpwRequestDTO;
import com.POM.MatNam.user.dto.LoginRequestDTO;
import com.POM.MatNam.user.dto.SignupRequestDTO;
import com.POM.MatNam.user.dto.UpdateRequestDTO;
import com.POM.MatNam.user.dto.User;
import com.POM.MatNam.user.dto.UserAuth;
import com.POM.MatNam.user.service.JwtService;
import com.POM.MatNam.user.service.MailSendService;
import com.POM.MatNam.user.service.UserService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = { "*" })
public class UserController {
	@Autowired
	private UserService userService;
	
	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private MailSendService mailSendService;

	@Autowired
	private ReviewDao reviewDao;
	
	@Autowired
	private StoreResDao storeResDao;
	
	@Autowired
	private UserDao userDao;	
	
	@PostMapping
	@ApiOperation(value = "?????? ??????")
	public Object signup(@Valid @RequestBody SignupRequestDTO request) {
		ResponseEntity<BasicResponse> response = null;
		Map<String, Object> errors = new HashMap<>();
		int check = userService.duplicateCheck(request.getEmail(), request.getNickname());
		if (check == 1) {
			errors.put("field", "email");
			errors.put("data", request.getEmail());
			final ErrorResponse result = setErrors("E-4001", "?????? ???????????? ??????????????????.", errors);

			response = new ResponseEntity<BasicResponse>(result, HttpStatus.CONFLICT);
		} else if (check == 2) {
			errors.put("field", "nickname");
			errors.put("data", request.getNickname());
			final ErrorResponse result = setErrors("E-4002", "?????? ???????????? ??????????????????.", errors);

			response = new ResponseEntity<BasicResponse>(result, HttpStatus.CONFLICT);
		} else {
			String key = mailSendService.getKey(false, 20);
			
			try {
				UserAuth user = userService.signup(request,key);
				mailSendService.mailSendWithUserKey(user.getEmail(), user.getNickname(), key, user.getId());
				 
			}catch(MessagingException e) {
				errors.put("field", "sendMail");

                final ErrorResponse result = setErrors("E-4006", "?????? ????????? ??????????????????.", errors);

                return new ResponseEntity<>(result, HttpStatus.CONFLICT);
			}
			final BasicResponse result = new BasicResponse();
			result.status = "S-200";
			result.message = "??????????????? ??????????????????.";
			response = new ResponseEntity<BasicResponse>(result, HttpStatus.CREATED);

		}
		return response;
	}

	@PostMapping("/login") 
	@ApiOperation(value = "?????????")
	public Object login(@Valid @RequestBody LoginRequestDTO request, HttpServletResponse res) {
		ResponseEntity<BasicResponse> response = null;
		Map<String, Object> errors = new HashMap<>();
		int check = userService.login(request);
		if (check == 1) {
			errors.put("field", "email");
			errors.put("data", request.getEmail());
			final ErrorResponse result = setErrors("E-4003", "???????????? ?????? ??????????????????.", errors);

			response = new ResponseEntity<BasicResponse>(result, HttpStatus.NOT_FOUND);
		} else if (check == 2) {
			errors.put("field", "password");
			errors.put("data", request.getPassword());
			final ErrorResponse result = setErrors("E-4004", "??????????????? ???????????? ????????????.", errors);

			response = new ResponseEntity<BasicResponse>(result, HttpStatus.CONFLICT);
		} else {
			final BasicResponse result = new BasicResponse();
			User user = userService.selectByEmail(request.getEmail());
			String token = jwtService.create(user);
			String nickname = user.getNickname();
			res.setHeader("jwt-auth-token", token);
			res.setHeader("nickname", nickname);
			result.status = "S-200";
			result.message = "???????????? ??????????????????.";
			response = new ResponseEntity<BasicResponse>(result, HttpStatus.OK);
		}
		return response;
	}

	@GetMapping
	@ApiOperation(value = "?????? ?????? ??????")
	public Object select(@RequestParam String nickname) {
		ResponseEntity<BasicResponse> response = null;
		Map<String, Object> errors = new HashMap<>();
		User user = userService.selectByNickname(nickname);
		if (user == null) {
			errors.put("field", "nickname");
			errors.put("data", nickname);
			final ErrorResponse result = setErrors("E-4005", "?????? ????????? ???????????? ????????????.", errors);
			response = new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
		} else {
			final BasicResponse result = new BasicResponse();
			Map<String, Object> data = new HashMap<>();
			List<Review> reviews = reviewDao.findByNickname(nickname);
			result.status = "S-200";
			result.message = "?????? ?????? ????????? ??????????????????.";
			data.put("user", user);
			data.put("reviews",reviews);
			result.data = data;
			response = new ResponseEntity<>(result, HttpStatus.OK);
		}
		return response;
	}

	@PutMapping
	@ApiOperation(value = "?????? ?????? ??????")
	public Object update(@Valid @RequestBody UpdateRequestDTO request,
			@RequestHeader(value = "nickname", required = true) String nickname, HttpServletResponse res) {
		ResponseEntity<BasicResponse> response = null;
		Map<String, Object> errors = new HashMap<>();
		User checkUser = userService.selectByNickname(nickname);
		int check = userService.duplicateCheck("", request.getNickname());
		if (!checkUser.getNickname().equals(request.getNickname()) && check == 2) {
			errors.put("field", "nickname");
			errors.put("data", request.getNickname());
			final ErrorResponse result = setErrors("E-4002", "?????? ???????????? ??????????????????.", errors);
			response = new ResponseEntity<>(result, HttpStatus.CONFLICT);
		} else {
			final BasicResponse result = new BasicResponse();
			User user = userService.update(request, nickname);
			String token = jwtService.create(user);
			res.setHeader("jwt-auth-token", token);
			res.setHeader("nickname",user.getNickname());
			result.status = "S-200";
			result.message = "?????? ?????? ????????? ?????????????????????.";
			response = new ResponseEntity<>(result, HttpStatus.OK);
		}
		return response;
	}

	@DeleteMapping
	@ApiOperation(value = "????????????")
	public Object withDraw(@RequestParam String nickname) {
		ResponseEntity<BasicResponse> response = null;
		Map<String, Object> errors = new HashMap<>();
		User user = userService.selectByNickname(nickname);
		if (user == null) {
			errors.put("field", "nickname");
			errors.put("data", nickname);
			final ErrorResponse result = setErrors("E-4005", "?????? ????????? ???????????? ????????????.", errors);
			response = new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
		} else {
			userService.withdraw(nickname);
			final BasicResponse result = new BasicResponse();
			result.status = "S-200";
			result.message = "?????? ????????? ??????????????????.";
			response = new ResponseEntity<>(result, HttpStatus.NO_CONTENT);
		}
		return response;
	}
	
	@GetMapping("/auth")
	@ApiOperation(value="????????? ??????")
	public Object confirm(@RequestParam long id, @RequestParam String key) {
		 ResponseEntity<BasicResponse> response = null;
	        Map<String, Object> errors = new HashMap<>();
	        User user = userService.authentication(id, key);
	        if (user == null) {
	            errors.put("field", "key");
	            errors.put("data", key);
	            final ErrorResponse result = setErrors("E-4007", "????????? ????????? ??????????????????.", errors);
	            response = new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
	        } else {
	        	// Store_Res?????? ?????? ???????????? ??????
	        	Optional<StoreRes>optStore = storeResDao.findByNickname(user.getNickname());
	        	
	        	
	        	// ????????? ?????? ????????? ?????? ?????? ??????
	        	if(optStore.isPresent()) {
	        		User tempUser = userService.selectByNickname(user.getNickname());
	        		tempUser.setStore_id(optStore.get().getId());
	        		
	        		userDao.save(tempUser);
	        		
	        		final BasicResponse result = new BasicResponse();
	 	            result.status = "S-200";
	 	            result.message = "?????? ?????? ????????? ????????? ??????????????????.";
	 	            response = new ResponseEntity<>(result, HttpStatus.OK);
	        	// ????????? ?????? ?????? ??????
	        	}else {
	        		final BasicResponse result = new BasicResponse();
	 	            result.status = "S-200";
	 	            result.message = "?????? ?????? ????????? ????????? ??????????????????.";
	 	            response = new ResponseEntity<>(result, HttpStatus.OK);
	        	}
	        	
	           
	        }
	        return response;
	}
	
	@PostMapping("/user/findpw")
    @ApiOperation(value = "?????? ?????? ??????")
    public Object findpw(@RequestBody FindpwRequestDTO request) {
        ResponseEntity<BasicResponse> response = null;
        Map<String, Object> errors = new HashMap<>();
        String pw = userService.findPw(request.getEmail(), request.getNickname());
        if (pw.equals("email")) {
        	errors.put("field", "email");
            errors.put("data", request.getEmail());
            final ErrorResponse result = setErrors("E-4003", "???????????? ?????? ????????? ?????????.", errors);
            response = new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        } else if (pw.equals("nickname")) {
            errors.put("field", "nickname");
            errors.put("data", request.getNickname());
            final ErrorResponse result = setErrors("E-4008", "???????????? ???????????? ????????????.", errors);
            response = new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        } else {
            try {
                mailSendService.mailSendWithPassword(request.getEmail(), request.getNickname(), pw);
            } catch (MessagingException e) {
                errors.put("field", "sendMail");
                final ErrorResponse result = setErrors("E-4006", "?????? ????????? ??????????????????.", errors);
                return new ResponseEntity<>(result, HttpStatus.CONFLICT);
            }
            final BasicResponse result = new BasicResponse();
            result.status = "S-200";
            result.message = "???????????? ????????? ??????????????????.";
            response = new ResponseEntity<>(result, HttpStatus.OK);
        }

        return response;
    }

	private ErrorResponse setErrors(String status, String message, Map<String, Object> errors) {
		ErrorResponse res = new ErrorResponse();
		res.status = status;
		res.message = message;
		res.errors = errors;
		return res;
	}
}
