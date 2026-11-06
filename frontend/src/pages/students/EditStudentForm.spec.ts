import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import EditStudentForm from './EditStudentForm.vue'

const mocks = vi.hoisted(() => ({
  findStudentById: vi.fn(),
  deactivateStudent: vi.fn(),
  confirm: vi.fn()
}))

vi.mock('@/apis/student', () => ({
  deactivateStudent: mocks.deactivateStudent,
  findStudentById: mocks.findStudentById,
  updateStudent: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn() },
  ElMessageBox: { confirm: mocks.confirm }
}))

const student = {
  id: 4,
  username: 'j.smith@abc.edu',
  firstName: 'John',
  lastName: 'Smith',
  email: 'j.smith@abc.edu',
  enabled: true,
  teamName: 'Team1'
}

describe('EditStudentForm deactivation confirmation', () => {
  beforeEach(() => {
    mocks.findStudentById.mockResolvedValue({ data: student })
    mocks.deactivateStudent.mockReset()
    mocks.confirm.mockReset()
  })

  it('leaves the displayed student unchanged and sends no request when cancellation is selected', async () => {
    mocks.confirm.mockRejectedValue(new Error('cancel'))
    const wrapper = mount(EditStudentForm, {
      props: { studentId: 4 },
      global: {
        stubs: {
          'el-form': { template: '<form><slot /></form>' },
          'el-form-item': { template: '<section><slot /></section>' },
          'el-input': { props: ['modelValue'], template: '<input :value="modelValue" />' },
          'el-tag': { template: '<span><slot /></span>' },
          'el-button': {
            inheritAttrs: false,
            emits: ['click'],
            template: '<button @click="$emit(\'click\')"><slot /></button>'
          }
        }
      }
    })
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === 'Deactivate')!.trigger('click')
    await flushPromises()

    expect(mocks.confirm).toHaveBeenCalledOnce()
    expect(mocks.deactivateStudent).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Enabled')
    expect(wrapper.findAll('input').map((input) => input.element.value)).toContain('Team1')
  })
})
