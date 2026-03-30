package com.k.medtour.domain.profile.service;

import com.k.medtour.domain.admin.entity.AgencyProfile;
import com.k.medtour.domain.admin.repository.AgencyProfileRepository;
import com.k.medtour.domain.file.entity.FileEntity;
import com.k.medtour.domain.file.enums.FileCategory;
import com.k.medtour.domain.file.repository.FileRepository;
import com.k.medtour.domain.profile.dto.PortfolioResponse;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ProfileService {

    private final AgencyProfileRepository agencyProfileRepository;
    private final FileRepository fileRepository;

    public PortfolioResponse getPortfolio(Long organizationId) {
        AgencyProfile profile = agencyProfileRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));

        List<FileEntity> portfolioFiles = fileRepository
                .findByCategoryAndDeletedAtIsNull(FileCategory.PORTFOLIO);

        List<PortfolioResponse.PortfolioItem> items = portfolioFiles.stream()
                .map(file -> new PortfolioResponse.PortfolioItem(
                        file.getId(),
                        file.getOriginalName(),
                        file.getUrl(),
                        file.getCategory().name(),
                        null
                ))
                .toList();

        return new PortfolioResponse(
                profile.getId(),
                profile.getName(),
                profile.getDescription(),
                items
        );
    }
}
