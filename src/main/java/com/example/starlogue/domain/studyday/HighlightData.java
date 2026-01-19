package com.example.starlogue.domain.studyday;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public  class HighlightData {

    private MvpPeriod mvpPeriod;
    private List<CrisisEvent> crisisEvents;
    private String aiSuggestion;
}
