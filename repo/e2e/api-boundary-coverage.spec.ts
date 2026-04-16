import { expect, test } from '@playwright/test'
import {
  apiBaseUrl,
  apiLogin,
  createPublishedQuizAsInstructor,
  createUserAsAdmin,
  expectHttpStatus,
} from './helpers/fullstack-fixtures'

function localDateTime(minutesOffset: number): string {
  const d = new Date(Date.now() + minutesOffset * 60_000)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}T${hh}:${min}`
}

async function expectSuccessEnvelope(response: { json: () => Promise<any> }, expectedSuccess: boolean) {
  const body = await response.json()
  expect(body).toHaveProperty('success', expectedSuccess)
  return body
}

test.describe('Real network API boundary coverage', () => {
  test('covers high-value endpoints with APIRequestContext', async ({ request }) => {
    const base = apiBaseUrl()

    const adminToken = await apiLogin(request, 'admin', 'Admin@12345678')
    const studentToken = await apiLogin(request, 'student.integration', 'Student@12345')
    const instructorToken = await apiLogin(request, 'instructor.quiz.api', 'Instructor@12345')
    const curatorToken = await apiLogin(request, 'curator.integration', 'Curator@12345')

    // Auth endpoints
    {
      const refreshRes = await request.post(`${base}/api/auth/refresh`, {
        data: { refreshToken: 'not-a-real-token' },
      })
      expectHttpStatus(refreshRes.status(), [401, 400])

      const logoutRes = await request.post(`${base}/api/auth/logout`, {
        headers: { Authorization: `Bearer ${adminToken}` },
      })
      expectHttpStatus(logoutRes.status(), [200])
    }

    // User / role / audit endpoints
    {
      const tempUser = await createUserAsAdmin(request, adminToken, 'TEACHING_ASSISTANT', 'api.coverage.tmp')

      const usersRes = await request.get(`${base}/api/users`, {
        headers: { Authorization: `Bearer ${adminToken}` },
      })
      expectHttpStatus(usersRes.status(), [200])
      const usersBody = await expectSuccessEnvelope(usersRes, true)
      expect(usersBody).toHaveProperty('data')

      const usersForbiddenRes = await request.get(`${base}/api/users`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(usersForbiddenRes.status(), [403])
      await expectSuccessEnvelope(usersForbiddenRes, false)

      const rolesRes = await request.get(`${base}/api/roles`, {
        headers: { Authorization: `Bearer ${adminToken}` },
      })
      expectHttpStatus(rolesRes.status(), [200])
      const rolesBody = await expectSuccessEnvelope(rolesRes, true)
      expect(Array.isArray(rolesBody.data)).toBe(true)
      const taRole = (rolesBody.data as Array<{ id: number; name: string }>).find((r) => r.name === 'TEACHING_ASSISTANT')
      expect(taRole).toBeTruthy()

      if (taRole) {
        const revokeRoleRes = await request.delete(`${base}/api/users/${tempUser.id}/roles/${taRole.id}`, {
          headers: { Authorization: `Bearer ${adminToken}` },
        })
        expectHttpStatus(revokeRoleRes.status(), [200, 404, 409])
      }

      const auditRes = await request.get(`${base}/api/audit-logs`, {
        headers: { Authorization: `Bearer ${adminToken}` },
      })
      expectHttpStatus(auditRes.status(), [200])
      await expectSuccessEnvelope(auditRes, true)

      const permissionHistoryRes = await request.get(`${base}/api/permission-change-history`, {
        headers: { Authorization: `Bearer ${adminToken}` },
      })
      expectHttpStatus(permissionHistoryRes.status(), [200])
      await expectSuccessEnvelope(permissionHistoryRes, true)

      const deleteTempUserRes = await request.delete(`${base}/api/users/${tempUser.id}`, {
        headers: { Authorization: `Bearer ${adminToken}` },
      })
      expectHttpStatus(deleteTempUserRes.status(), [200, 404, 409])
    }

    // Catalog / content / media endpoints
    {
      const catalogRes = await request.get(`${base}/api/catalog`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(catalogRes.status(), [200])
      const catalogBody = await expectSuccessEnvelope(catalogRes, true)
      const firstCatalogId = Array.isArray(catalogBody.data) && catalogBody.data.length > 0
        ? catalogBody.data[0].id as number
        : 0
      if (firstCatalogId > 0) {
        const catalogItemRes = await request.get(`${base}/api/catalog/${firstCatalogId}`, {
          headers: { Authorization: `Bearer ${studentToken}` },
        })
        expectHttpStatus(catalogItemRes.status(), [200])
        await expectSuccessEnvelope(catalogItemRes, true)
      }

      const contentRes = await request.get(`${base}/api/content`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
      })
      expectHttpStatus(contentRes.status(), [200])
      await expectSuccessEnvelope(contentRes, true)

      const contentPublishRes = await request.post(`${base}/api/content/999999/publish`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
      })
      expectHttpStatus(contentPublishRes.status(), [404, 409])

      const mediaRes = await request.get(`${base}/api/content/media-metadata/999999`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(mediaRes.status(), [404, 200])
    }

    // Crawl source / rules / runs endpoints
    let sourceId = 0
    {
      const sourceCreateRes = await request.post(`${base}/api/crawl-sources`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
        data: {
          name: `api-coverage-source-${Date.now()}`,
          baseUrl: 'https://example.org',
          description: 'Created by Playwright API coverage',
          rateLimitPerMinute: 30,
          requiresAuth: false,
        },
      })
      expectHttpStatus(sourceCreateRes.status(), [201])
      const sourceJson = await expectSuccessEnvelope(sourceCreateRes, true)
      sourceId = sourceJson.data.id as number
      expect(sourceJson.data.name).toContain('api-coverage-source')

      const listSourcesRes = await request.get(`${base}/api/crawl-sources`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
      })
      expectHttpStatus(listSourcesRes.status(), [200])
      await expectSuccessEnvelope(listSourcesRes, true)

      const getSourceRes = await request.get(`${base}/api/crawl-sources/${sourceId}`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
      })
      expectHttpStatus(getSourceRes.status(), [200])
      const getSourceBody = await expectSuccessEnvelope(getSourceRes, true)
      expect(getSourceBody.data.id).toBe(sourceId)

      const updateSourceRes = await request.put(`${base}/api/crawl-sources/${sourceId}`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
        data: {
          name: `api-coverage-source-updated-${Date.now()}`,
          baseUrl: 'https://example.org',
          description: 'Updated by Playwright API coverage',
          rateLimitPerMinute: 40,
          requiresAuth: false,
        },
      })
      expectHttpStatus(updateSourceRes.status(), [200])

      const listRulesRes = await request.get(`${base}/api/crawl-sources/${sourceId}/rules`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
      })
      expectHttpStatus(listRulesRes.status(), [200])
      await expectSuccessEnvelope(listRulesRes, true)

      const createRuleRes = await request.post(`${base}/api/crawl-sources/${sourceId}/rules`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
        data: {
          extractionMethod: 'CSS_SELECTOR',
          ruleDefinition: { title: 'h1' },
          fieldMappings: { title: 'title' },
          notes: 'Coverage rule',
        },
      })
      expectHttpStatus(createRuleRes.status(), [201])
      const createRuleBody = await expectSuccessEnvelope(createRuleRes, true)
      expect(createRuleBody.data).toHaveProperty('versionNumber')

      const crawlRunsRes = await request.get(`${base}/api/crawl-runs`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
      })
      expectHttpStatus(crawlRunsRes.status(), [200])
      await expectSuccessEnvelope(crawlRunsRes, true)
    }

    // Schedule / timetable endpoints
    let scheduleId = 0
    {
      const createScheduleRes = await request.post(`${base}/api/schedules`, {
        headers: { Authorization: `Bearer ${studentToken}` },
        data: {
          title: 'API coverage schedule',
          startTime: localDateTime(60),
          endTime: localDateTime(120),
        },
      })
      expectHttpStatus(createScheduleRes.status(), [201])
      const scheduleJson = await expectSuccessEnvelope(createScheduleRes, true)
      scheduleId = scheduleJson.data.id as number
      expect(scheduleJson.data.title).toBe('API coverage schedule')

      const listScheduleRes = await request.get(`${base}/api/schedules`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(listScheduleRes.status(), [200])
      await expectSuccessEnvelope(listScheduleRes, true)

      const lockedPeriodsRes = await request.get(`${base}/api/locked-periods`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(lockedPeriodsRes.status(), [200])
      await expectSuccessEnvelope(lockedPeriodsRes, true)

      const moveRes = await request.post(`${base}/api/schedules/${scheduleId}/move`, {
        headers: { Authorization: `Bearer ${studentToken}` },
        data: {
          scheduleId: '00000000-0000-0000-0000-000000000001',
          newStartTime: localDateTime(90),
          newEndTime: localDateTime(150),
        },
      })
      expectHttpStatus(moveRes.status(), [200, 409, 400])

      const mergeRes = await request.post(`${base}/api/schedules/merge`, {
        headers: { Authorization: `Bearer ${studentToken}` },
        data: { scheduleIds: [scheduleId, scheduleId] },
      })
      expectHttpStatus(mergeRes.status(), [200, 400, 409])

      const splitRes = await request.post(`${base}/api/schedules/${scheduleId}/split`, {
        headers: { Authorization: `Bearer ${studentToken}` },
        data: { splitTime: localDateTime(105) },
      })
      expectHttpStatus(splitRes.status(), [200, 400, 409])

      const journalRes = await request.get(`${base}/api/schedules/change-journal`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(journalRes.status(), [200])
      await expectSuccessEnvelope(journalRes, true)

      const undoRes = await request.post(`${base}/api/schedules/undo`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(undoRes.status(), [200, 409])

      const redoRes = await request.post(`${base}/api/schedules/redo`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(redoRes.status(), [200, 409])
    }

    // Submission / grading / plagiarism / wrong-answer endpoints
    {
      const quizId = await createPublishedQuizAsInstructor(request, instructorToken)

      const listQuizzesRes = await request.get(`${base}/api/quizzes`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
      })
      expectHttpStatus(listQuizzesRes.status(), [200])
      await expectSuccessEnvelope(listQuizzesRes, true)

      const scheduleQuizRes = await request.put(`${base}/api/quizzes/${quizId}/schedule`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
        data: {
          releaseStart: localDateTime(10),
          releaseEnd: localDateTime(180),
        },
      })
      expectHttpStatus(scheduleQuizRes.status(), [200, 400])

      const startSubmissionRes = await request.post(`${base}/api/quizzes/${quizId}/submissions`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(startSubmissionRes.status(), [201, 409])

      const submissionId = startSubmissionRes.status() === 201
        ? (await expectSuccessEnvelope(startSubmissionRes, true)).data.id as number
        : 0

      if (submissionId > 0) {
        const autosaveRes = await request.put(`${base}/api/submissions/${submissionId}/autosave`, {
          headers: { Authorization: `Bearer ${studentToken}` },
          data: {
            answers: [{ questionId: 1, answerText: 'coverage answer' }],
          },
        })
        expectHttpStatus(autosaveRes.status(), [200, 400, 409])

        const submitRes = await request.put(`${base}/api/submissions/${submissionId}/submit`, {
          headers: { Authorization: `Bearer ${studentToken}` },
          data: {
            answers: [{ questionId: 1, answerText: 'final coverage answer' }],
          },
        })
        expectHttpStatus(submitRes.status(), [200, 400, 409])

        const getSubmissionRes = await request.get(`${base}/api/submissions/${submissionId}`, {
          headers: { Authorization: `Bearer ${studentToken}` },
        })
        expectHttpStatus(getSubmissionRes.status(), [200, 403, 404])
        if (getSubmissionRes.status() === 200) {
          await expectSuccessEnvelope(getSubmissionRes, true)
        }

        const feedbackRes = await request.get(`${base}/api/submissions/${submissionId}/feedback`, {
          headers: { Authorization: `Bearer ${studentToken}` },
        })
        expectHttpStatus(feedbackRes.status(), [200, 404, 409])
        if (feedbackRes.status() === 200) {
          await expectSuccessEnvelope(feedbackRes, true)
        }
      }

      const queueRes = await request.get(`${base}/api/grading/queue`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
      })
      expectHttpStatus(queueRes.status(), [200])
      await expectSuccessEnvelope(queueRes, true)

      const gradingSubmissionRes = await request.get(`${base}/api/grading/submissions/1`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
      })
      expectHttpStatus(gradingSubmissionRes.status(), [200, 404])

      const gradeRes = await request.post(`${base}/api/grading/submissions/1/grade`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
        data: {
          submissionAnswerId: '00000000-0000-0000-0000-000000000001',
          score: 1,
          feedback: 'coverage',
        },
      })
      expectHttpStatus(gradeRes.status(), [200, 400, 404])

      const rubricRes = await request.post(`${base}/api/grading/submissions/1/rubric-scores`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
        data: {
          rubricScores: [],
        },
      })
      expectHttpStatus(rubricRes.status(), [200, 400, 404])

      const plagiarismChecksRes = await request.get(`${base}/api/plagiarism/checks`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
      })
      expectHttpStatus(plagiarismChecksRes.status(), [200])
      await expectSuccessEnvelope(plagiarismChecksRes, true)

      const plagiarismCheckRes = await request.get(`${base}/api/plagiarism/checks/1`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
      })
      expectHttpStatus(plagiarismCheckRes.status(), [200, 404])

      const plagiarismMatchesRes = await request.get(`${base}/api/plagiarism/checks/1/matches`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
      })
      expectHttpStatus(plagiarismMatchesRes.status(), [200, 404])

      const wrongAnswersRes = await request.get(`${base}/api/wrong-answers`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(wrongAnswersRes.status(), [200])
      await expectSuccessEnvelope(wrongAnswersRes, true)

      const wrongAnswerByQuestionRes = await request.get(`${base}/api/wrong-answers/1`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(wrongAnswerByQuestionRes.status(), [200, 404])

      const listBanksRes = await request.get(`${base}/api/question-banks`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
      })
      expectHttpStatus(listBanksRes.status(), [200])
      await expectSuccessEnvelope(listBanksRes, true)

      const listTagsRes = await request.get(`${base}/api/knowledge-tags`, {
        headers: { Authorization: `Bearer ${instructorToken}` },
      })
      expectHttpStatus(listTagsRes.status(), [200])
      await expectSuccessEnvelope(listTagsRes, true)
    }

    // Cleanup endpoint coverage
    if (sourceId > 0) {
      const deleteSourceRes = await request.delete(`${base}/api/crawl-sources/${sourceId}`, {
        headers: { Authorization: `Bearer ${curatorToken}` },
      })
      expectHttpStatus(deleteSourceRes.status(), [200, 409, 404])
    }

    if (scheduleId > 0) {
      const createLockedPeriodRes = await request.post(`${base}/api/locked-periods`, {
        headers: { Authorization: `Bearer ${studentToken}` },
        data: {
          title: 'API coverage lock',
          startTime: localDateTime(240),
          endTime: localDateTime(300),
          reason: 'coverage',
        },
      })
      expectHttpStatus(createLockedPeriodRes.status(), [201, 409, 400])
      if (createLockedPeriodRes.status() === 201) {
        const createdLockBody = await expectSuccessEnvelope(createLockedPeriodRes, true)
        const lockId = createdLockBody.data.id as number
        const deleteLockRes = await request.delete(`${base}/api/locked-periods/${lockId}`, {
          headers: { Authorization: `Bearer ${studentToken}` },
        })
        expectHttpStatus(deleteLockRes.status(), [200, 404, 409])
      }

      const deleteScheduleRes = await request.delete(`${base}/api/schedules/${scheduleId}`, {
        headers: { Authorization: `Bearer ${studentToken}` },
      })
      expectHttpStatus(deleteScheduleRes.status(), [200, 404])
    }
  })
})
