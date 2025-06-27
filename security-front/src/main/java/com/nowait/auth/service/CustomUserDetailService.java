package com.nowait.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.nowaiting.user.entity.MemberDetails;
import com.nowaiting.user.entity.User;
import com.nowaiting.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email).orElseThrow();
		return MemberDetails.create(user);
	}

	public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
		User member = userRepository.findById(id).orElseThrow();
		return MemberDetails.create(member);
	}
}
