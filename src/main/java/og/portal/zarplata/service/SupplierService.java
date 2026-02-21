package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import og.portal.zarplata.dto.SupplierSettingDTO;
import og.portal.zarplata.mapper.SupplierMapper;
import og.portal.zarplata.repository.SupplierSettingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierSettingRepository supplierSettingRepository;
    private final SupplierMapper supplierMapper;

    public List<SupplierSettingDTO> getAllSupplierSettings() {
        return supplierSettingRepository.findAll().stream()
                .map(supplierMapper::toDto)
                .collect(Collectors.toList());
    }
}