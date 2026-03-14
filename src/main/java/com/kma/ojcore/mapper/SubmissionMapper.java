package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo;
import com.kma.ojcore.dto.response.submissions.SubmissionDetailsSdo;
import com.kma.ojcore.entity.Submission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {

    SubmissionBasicSdo toBasicSdo(Submission submission);

    SubmissionDetailsSdo toDetailsSdo(Submission submission);
}
