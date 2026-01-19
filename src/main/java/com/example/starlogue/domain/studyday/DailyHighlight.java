package com.example.starlogue.domain.studyday;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DailyHighlight {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "highlight_json", columnDefinition = "jsonb")
    private HighlightData data;

}
