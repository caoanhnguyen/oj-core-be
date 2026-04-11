package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.request.organizations.OrganizationCreateSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationUpdateSdi;
import com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationMemberSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationJoinRequestSdo;
import com.kma.ojcore.entity.Organization;
import com.kma.ojcore.entity.OrganizationMember;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "slug", ignore = true)
    Organization toEntity(OrganizationCreateSdi sdi);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "members", ignore = true)
    Organization toEntityFromUpdate(OrganizationUpdateSdi sdi);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    OrganizationSdo toSdo(Organization organization);

    OrganizationBasicSdo toBasicSdo(Organization organization);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName", defaultValue = "")
    OrganizationMemberSdo toMemberSdo(OrganizationMember member);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName", defaultValue = "")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "orgId", source = "organization.id")
    @Mapping(target = "orgName", source = "organization.name")
    @Mapping(target = "orgSlug", source = "organization.slug")
    @Mapping(target = "status", source = "memberStatus")
    @Mapping(target = "message", source = "joinRequestMessage")
    @Mapping(target = "requestedAt", source = "createdDate")
    OrganizationJoinRequestSdo toJoinRequestSdo(OrganizationMember member);
}