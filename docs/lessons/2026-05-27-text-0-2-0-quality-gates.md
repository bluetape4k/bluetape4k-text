# Text 0.2.0 품질 Gate

## 배경

Milestone 0.2.0에는 tokenizer 정확도, dictionary update 절차, README에서 연결되는 품질
근거, request validation 회귀 coverage를 확인할 구체적인 품질 gate가 필요했다.

## 결정

큰 외부 NLP benchmark 주장을 내세우기보다 source control에 들어간 deterministic test와 내부
문서를 사용한다. Dictionary runtime reload는 0.2.0 범위에서 제외하고 0.3.0 issue로
남긴다.

## 결과

Korean/Japanese 혼합 tokenizer fixture test, sanitized oversized request test, 품질 gate
spec, dictionary update plan, README locale set에서 연결되는 quality report를 추가했다.

## 향후 guard

0.2.0 report를 근거로 넓은 NLP 정확도를 주장하지 않는다. 더 큰 점수화된 corpus가 생기기
전까지는 release 품질 gate로 취급한다.
