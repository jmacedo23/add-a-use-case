/* global cy, describe, beforeEach, it */

// UC-STU-deactivate-student cancellation: confirmation is required before the lifecycle request is sent.
describe('Deactivate student', () => {
  const student = {
    id: 4,
    username: 'j.smith@abc.edu',
    firstName: 'John',
    lastName: 'Smith',
    email: 'j.smith@abc.edu',
    enabled: true,
    sectionId: 2,
    sectionName: '2023-2024',
    teamId: 1,
    teamName: 'Team1'
  }

  beforeEach(() => {
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('POST', '**/students/search*', {
      body: {
        flag: true,
        code: 200,
        message: 'Find students successfully',
        data: {
          content: [student],
          number: 0,
          size: 50,
          totalElements: 1
        }
      }
    }).as('students')
    cy.intercept('GET', '**/students/4', {
      body: { flag: true, code: 200, message: 'Find student successfully', data: student }
    }).as('student')
    cy.intercept('POST', '**/students/4/deactivation').as('deactivate')
  })

  it('does not deactivate when the instructor cancels the confirmation', () => {
    cy.visit('/students')
    cy.wait('@students')
    cy.get('[data-cy=edit-student-4]').click()
    cy.wait('@student')

    cy.get('[data-cy=deactivate-student]').click()
    cy.get('.el-message-box').contains('button', 'Cancel').click()

    cy.get('@deactivate.all').should('have.length', 0)
    cy.get('.el-dialog').should('contain', 'Enabled').and('contain', 'Team1')
  })
})
