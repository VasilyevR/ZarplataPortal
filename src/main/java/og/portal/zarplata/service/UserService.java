package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import og.portal.zarplata.dto.UserDTO;
import og.portal.zarplata.mapper.UserMapper;
import og.portal.zarplata.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDTO getUserByLogin(String login) {
        return userRepository.findByLogin(login)
                .map(userMapper::toDto)
                .orElse(UserDTO.builder().login(login).build());
    }

    public List<UserDTO> getAllUsersSortedByLogin() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "login")).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }
}