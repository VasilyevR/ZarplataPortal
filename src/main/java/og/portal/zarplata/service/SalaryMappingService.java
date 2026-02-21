package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import og.portal.zarplata.dto.SalaryColumnMappingDTO;
import og.portal.zarplata.mapper.SalaryColumnMapper;
import og.portal.zarplata.repository.SalaryColumnMappingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryMappingService {

    private final SalaryColumnMappingRepository repository;
    private final SalaryColumnMapper salaryColumnMapper;

    public List<SalaryColumnMappingDTO> getVisibleColumns() {
        return repository.findByVisibleTrueOrderByExcelColIndexAsc().stream()
                .map(salaryColumnMapper::toDto)
                .collect(Collectors.toList());
    }
}