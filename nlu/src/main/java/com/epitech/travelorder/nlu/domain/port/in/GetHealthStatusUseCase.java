package com.epitech.travelorder.nlu.domain.port.in;

import com.epitech.travelorder.common.domain.nlu.NluStatus;

public interface GetHealthStatusUseCase {
  NluStatus getStatus();
}

