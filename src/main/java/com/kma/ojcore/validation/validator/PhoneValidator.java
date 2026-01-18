package com.kma.ojcore.validation.validator;

import com.kma.ojcore.validation.annotation.PhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<PhoneNumber, String> {

    @Override
    public void initialize(PhoneNumber phoneNumberNo) {
    }

    @Override
    public boolean isValid(String phoneNo, ConstraintValidatorContext cxt) {
        //validate phone numbers of format "0902345345"
        if (phoneNo == null || phoneNo.isEmpty()) {
            return true; // hoặc false, tùy yêu cầu: true nếu cho phép null, false nếu bắt buộc có số điện thoại
        }else if (phoneNo.matches("\\d{10}")) return true;
            //validating phone number with -, . or spaces: 090-234-4567
        else if(phoneNo.matches("\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4}")) return true;
            //validating phone number with extension length from 3 to 5
        else //return false if nothing matches the input
            if(phoneNo.matches("\\d{3}-\\d{3}-\\d{4}\\s(x|(ext))\\d{3,5}")) return true;
                //validating phone number where area code is in braces ()
            else return phoneNo.matches("\\(\\d{3}\\)-\\d{3}-\\d{4}");
    }

}